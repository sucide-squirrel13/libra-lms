package com.lax.lms;

import com.lax.lms.repository.BookRepository;
import com.lax.lms.repository.BorrowingRepository;
import com.lax.lms.repository.LibraryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:lms-concurrency-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.security.oauth2.client.registration.google.client-id=dummy-client-id",
        "spring.security.oauth2.client.registration.google.client-secret=dummy-client-secret"
})
class BorrowingConcurrencyIntegrationTest {

    @Autowired
    private BorrowingService borrowingService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BorrowingRepository borrowingRepository;

    @Autowired
    private LibraryUserRepository userRepository;

    private LibraryUser studentUser;
    private Book testBook;

    @BeforeEach
    void setUp() {
        borrowingRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();

        studentUser = new LibraryUser();
        studentUser.setFullName("Test Student");
        studentUser.setEmail("student.test@example.com");
        studentUser.setPassword("password");
        studentUser.setRole("USER");
        studentUser = userRepository.save(studentUser);

        testBook = new Book();
        testBook.setTitle("Concurrent Systems in Practice");
        testBook.setAuthor("Concurrency Expert");
        testBook.setIsbn("ISBN-CONC-001");
        testBook.setCategory("Computer Science");
        testBook.setQuantity(5);
        testBook.setAvailableCopies(4); // 1 copy borrowed
        testBook = bookRepository.save(testBook);
    }

    private Borrowing createActiveBorrowing() {
        Borrowing borrowing = new Borrowing();
        borrowing.setUser(studentUser);
        borrowing.setBook(testBook);
        borrowing.setBorrowDate(LocalDate.now().minusDays(2));
        borrowing.setDueDate(LocalDate.now().plusDays(12));
        borrowing.setStatus("BORROWED");
        return borrowingRepository.save(borrowing);
    }

    @Test
    @DisplayName("Two concurrent student return requests: exactly one succeeds, one fails with already returned, copies incremented once")
    void twoConcurrentStudentReturns_onlyOneSucceeds() throws Exception {
        Borrowing borrowing = createActiveBorrowing();
        Long borrowingId = borrowing.getId();
        Long userId = studentUser.getId();

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicReference<Throwable> failureReason = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    borrowingService.returnBook(borrowingId, userId);
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    failureCount.incrementAndGet();
                    failureReason.set(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Fire simultaneous requests
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get())
                .as("Exactly one return request must succeed")
                .isEqualTo(1);
        assertThat(failureCount.get())
                .as("The competing request must fail")
                .isEqualTo(1);
        assertThat(failureReason.get())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been returned");

        // Verify database state: copies must be 5 (incremented from 4 to 5, NOT 6)
        Book reloadedBook = bookRepository.findById(testBook.getId()).orElseThrow();
        assertThat(reloadedBook.getAvailableCopies())
                .as("Available copies must be incremented exactly once")
                .isEqualTo(5);

        Borrowing reloadedBorrowing = borrowingRepository.findById(borrowingId).orElseThrow();
        assertThat(reloadedBorrowing.getStatus()).isEqualTo("RETURNED");
        assertThat(reloadedBorrowing.getReturnDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Concurrent student return and librarian return: exactly one succeeds, copies incremented once")
    void concurrentStudentAndLibrarianReturn_onlyOneSucceeds() throws Exception {
        Borrowing borrowing = createActiveBorrowing();
        Long borrowingId = borrowing.getId();
        Long userId = studentUser.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicReference<Throwable> failureReason = new AtomicReference<>();

        // Thread 1: Student return
        executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                borrowingService.returnBook(borrowingId, userId);
                successCount.incrementAndGet();
            } catch (Throwable t) {
                failureCount.incrementAndGet();
                failureReason.set(t);
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: Librarian return
        executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                borrowingService.returnBookByLibrarian(borrowingId, "librarian@libra.lms");
                successCount.incrementAndGet();
            } catch (Throwable t) {
                failureCount.incrementAndGet();
                failureReason.set(t);
            } finally {
                doneLatch.countDown();
            }
        });

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Fire simultaneous requests
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get())
                .as("Exactly one return request must succeed")
                .isEqualTo(1);
        assertThat(failureCount.get())
                .as("The competing request must fail")
                .isEqualTo(1);
        assertThat(failureReason.get())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been returned");

        Book reloadedBook = bookRepository.findById(testBook.getId()).orElseThrow();
        assertThat(reloadedBook.getAvailableCopies())
                .as("Available copies must be incremented exactly once")
                .isEqualTo(5);

        Borrowing reloadedBorrowing = borrowingRepository.findById(borrowingId).orElseThrow();
        assertThat(reloadedBorrowing.getStatus()).isEqualTo("RETURNED");
    }

    @Test
    @DisplayName("Two concurrent librarian return requests: exactly one succeeds, copies incremented once")
    void twoConcurrentLibrarianReturns_onlyOneSucceeds() throws Exception {
        Borrowing borrowing = createActiveBorrowing();
        Long borrowingId = borrowing.getId();

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicReference<Throwable> failureReason = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    borrowingService.returnBookByLibrarian(borrowingId, "librarian@libra.lms");
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    failureCount.incrementAndGet();
                    failureReason.set(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get())
                .as("Exactly one librarian return must succeed")
                .isEqualTo(1);
        assertThat(failureCount.get())
                .as("The competing request must fail")
                .isEqualTo(1);
        assertThat(failureReason.get())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been returned");

        Book reloadedBook = bookRepository.findById(testBook.getId()).orElseThrow();
        assertThat(reloadedBook.getAvailableCopies()).isEqualTo(5);

        Borrowing reloadedBorrowing = borrowingRepository.findById(borrowingId).orElseThrow();
        assertThat(reloadedBorrowing.getStatus()).isEqualTo("RETURNED");
    }
}
