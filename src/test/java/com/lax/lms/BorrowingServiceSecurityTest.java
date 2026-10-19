package com.lax.lms;

import com.lax.lms.repository.BookRepository;
import com.lax.lms.repository.BorrowingRepository;
import com.lax.lms.repository.LibraryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowingServiceSecurityTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BorrowingRepository borrowingRepository;

    @Mock
    private LibraryUserRepository userRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private BorrowingService borrowingService;

    private LibraryUser regularUser;
    private LibraryUser anotherUser;
    private LibraryUser librarianUser;
    private Book testBook;

    @BeforeEach
    void setUp() {
        regularUser = new LibraryUser();
        regularUser.setFullName("Regular Reader");
        regularUser.setEmail("reader@example.com");
        regularUser.setRole("USER");
        ReflectionTestUtils.setField(regularUser, "id", 1L);

        anotherUser = new LibraryUser();
        anotherUser.setFullName("Another Reader");
        anotherUser.setEmail("another@example.com");
        anotherUser.setRole("USER");
        ReflectionTestUtils.setField(anotherUser, "id", 3L);

        librarianUser = new LibraryUser();
        librarianUser.setFullName("Head Librarian");
        librarianUser.setEmail("librarian@example.com");
        librarianUser.setRole("LIBRARIAN");
        ReflectionTestUtils.setField(librarianUser, "id", 2L);

        testBook = new Book();
        testBook.setTitle("Spring Security in Action");
        testBook.setAuthor("Laurentiu Spilca");
        testBook.setQuantity(5);
        testBook.setAvailableCopies(5);
        ReflectionTestUtils.setField(testBook, "id", 10L);
    }

    @Test
    @DisplayName("borrowBook succeeds when caller is ROLE_USER and records SUCCESS audit")
    void borrowBook_whenRoleUser_succeeds() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
        when(bookRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testBook));
        when(borrowingRepository.existsByUserIdAndBookIdAndStatus(1L, 10L, "BORROWED")).thenReturn(false);

        borrowingService.borrowBook(1L, 10L);

        assertThat(testBook.getAvailableCopies()).isEqualTo(4);
        verify(borrowingRepository).save(any(Borrowing.class));
        verify(bookRepository).save(testBook);
        verify(auditService).record(eq("reader@example.com"), eq("BOOK_BORROW"), any(), eq("SUCCESS"));
    }

    @Test
    @DisplayName("borrowBook throws IllegalStateException and records FAILURE audit when caller is LIBRARIAN without modifying state")
    void borrowBook_whenLibrarian_throwsAndAuditsFailure() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(librarianUser));

        assertThatThrownBy(() -> borrowingService.borrowBook(2L, 10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only regular users can borrow books");

        assertThat(testBook.getAvailableCopies()).isEqualTo(5);
        verify(bookRepository, never()).findByIdForUpdate(anyLong());
        verify(borrowingRepository, never()).save(any());
        verify(bookRepository, never()).save(any());
        verify(auditService).record(eq("librarian@example.com"), eq("BOOK_BORROW"),
                contains("Only regular users can borrow books"), eq("FAILURE"));
    }

    @Test
    @DisplayName("borrowBook fails when no copies are available without modifying state and audits FAILURE")
    void borrowBook_whenBookUnavailable_throwsAndAuditsFailure() {
        testBook.setAvailableCopies(0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
        when(bookRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testBook));

        assertThatThrownBy(() -> borrowingService.borrowBook(1L, 10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("currently unavailable");

        assertThat(testBook.getAvailableCopies()).isEqualTo(0);
        verify(borrowingRepository, never()).save(any());
        verify(bookRepository, never()).save(any());
        verify(auditService).record(eq("reader@example.com"), eq("BOOK_BORROW"),
                contains("unavailable"), eq("FAILURE"));
    }

    @Test
    @DisplayName("borrowBook fails when user has already borrowed the book and audits FAILURE without modifying state")
    void borrowBook_whenAlreadyBorrowed_throwsAndAuditsFailure() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
        when(bookRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testBook));
        when(borrowingRepository.existsByUserIdAndBookIdAndStatus(1L, 10L, "BORROWED")).thenReturn(true);

        assertThatThrownBy(() -> borrowingService.borrowBook(1L, 10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already borrowed");

        assertThat(testBook.getAvailableCopies()).isEqualTo(5);
        verify(borrowingRepository, never()).save(any());
        verify(bookRepository, never()).save(any());
        verify(auditService).record(eq("reader@example.com"), eq("BOOK_BORROW"),
                contains("already borrowed"), eq("FAILURE"));
    }

    @Test
    @DisplayName("returnBook succeeds when caller is ROLE_USER and owns the borrowing and records SUCCESS audit")
    void returnBook_whenRoleUserAndOwner_succeeds() {
        testBook.setAvailableCopies(4);
        Borrowing borrowing = new Borrowing();
        borrowing.setUser(regularUser);
        borrowing.setBook(testBook);
        borrowing.setStatus("BORROWED");
        borrowing.setBorrowDate(LocalDate.now().minusDays(3));

        when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
        when(borrowingRepository.findById(100L)).thenReturn(Optional.of(borrowing));
        when(bookRepository.findByIdForUpdate(testBook.getId())).thenReturn(Optional.of(testBook));

        borrowingService.returnBook(100L, 1L);

        assertThat(borrowing.getStatus()).isEqualTo("RETURNED");
        assertThat(borrowing.getReturnDate()).isEqualTo(LocalDate.now());
        assertThat(testBook.getAvailableCopies()).isEqualTo(5);
        verify(borrowingRepository).save(borrowing);
        verify(bookRepository).save(testBook);
        verify(auditService).record(eq("reader@example.com"), eq("BOOK_RETURN"), any(), eq("SUCCESS"));
    }

    @Test
    @DisplayName("returnBook throws IllegalStateException and records FAILURE audit when caller is LIBRARIAN without modifying state")
    void returnBook_whenLibrarian_throwsAndAuditsFailure() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(librarianUser));

        assertThatThrownBy(() -> borrowingService.returnBook(100L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only regular users can return books");

        assertThat(testBook.getAvailableCopies()).isEqualTo(5);
        verify(borrowingRepository, never()).findById(anyLong());
        verify(borrowingRepository, never()).save(any());
        verify(bookRepository, never()).save(any());
        verify(auditService).record(eq("librarian@example.com"), eq("BOOK_RETURN"),
                contains("Only regular users can return books"), eq("FAILURE"));
    }

    @Test
    @DisplayName("returnBook throws IllegalStateException when user attempts to return another user's borrowing record")
    void returnBook_whenUserDoesNotOwnBorrowing_throwsAndAuditsFailure() {
        testBook.setAvailableCopies(4);
        Borrowing borrowing = new Borrowing();
        borrowing.setUser(anotherUser); // Owned by another user (id: 3L)
        borrowing.setBook(testBook);
        borrowing.setStatus("BORROWED");
        borrowing.setBorrowDate(LocalDate.now().minusDays(2));

        when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
        when(borrowingRepository.findById(100L)).thenReturn(Optional.of(borrowing));

        assertThatThrownBy(() -> borrowingService.returnBook(100L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot return another user's borrowing");

        assertThat(borrowing.getStatus()).isEqualTo("BORROWED");
        assertThat(borrowing.getReturnDate()).isNull();
        assertThat(testBook.getAvailableCopies()).isEqualTo(4);
        verify(bookRepository, never()).findByIdForUpdate(anyLong());
        verify(borrowingRepository, never()).save(any());
        verify(bookRepository, never()).save(any());
        verify(auditService).record(eq("reader@example.com"), eq("BOOK_RETURN"),
                contains("cannot return another user's borrowing"), eq("FAILURE"));
    }

    @Test
    @DisplayName("returnBook throws IllegalStateException when borrowing record is already RETURNED")
    void returnBook_whenAlreadyReturned_throwsAndAuditsFailure() {
        testBook.setAvailableCopies(5);
        LocalDate previousReturnDate = LocalDate.now().minusDays(1);
        Borrowing borrowing = new Borrowing();
        borrowing.setUser(regularUser);
        borrowing.setBook(testBook);
        borrowing.setStatus("RETURNED");
        borrowing.setBorrowDate(LocalDate.now().minusDays(10));
        borrowing.setReturnDate(previousReturnDate);

        when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
        when(borrowingRepository.findById(100L)).thenReturn(Optional.of(borrowing));

        assertThatThrownBy(() -> borrowingService.returnBook(100L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been returned");

        assertThat(borrowing.getStatus()).isEqualTo("RETURNED");
        assertThat(borrowing.getReturnDate()).isEqualTo(previousReturnDate);
        assertThat(testBook.getAvailableCopies()).isEqualTo(5);
        verify(bookRepository, never()).findByIdForUpdate(anyLong());
        verify(borrowingRepository, never()).save(any());
        verify(bookRepository, never()).save(any());
        verify(auditService).record(eq("reader@example.com"), eq("BOOK_RETURN"),
                contains("already been returned"), eq("FAILURE"));
    }

    @Test
    @DisplayName("returnBook throws IllegalStateException when availableCopies is already at quantity (inconsistent state)")
    void returnBook_whenAvailabilityInconsistent_throwsAndAuditsFailure() {
        testBook.setAvailableCopies(5); // equal to quantity 5
        Borrowing borrowing = new Borrowing();
        borrowing.setUser(regularUser);
        borrowing.setBook(testBook);
        borrowing.setStatus("BORROWED");
        borrowing.setBorrowDate(LocalDate.now().minusDays(3));

        when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
        when(borrowingRepository.findById(100L)).thenReturn(Optional.of(borrowing));
        when(bookRepository.findByIdForUpdate(testBook.getId())).thenReturn(Optional.of(testBook));

        assertThatThrownBy(() -> borrowingService.returnBook(100L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("availability is inconsistent");

        assertThat(borrowing.getStatus()).isEqualTo("BORROWED");
        assertThat(borrowing.getReturnDate()).isNull();
        assertThat(testBook.getAvailableCopies()).isEqualTo(5);
        verify(borrowingRepository, never()).save(any());
        verify(bookRepository, never()).save(any());
        verify(auditService).record(eq("reader@example.com"), eq("BOOK_RETURN"),
                contains("inconsistent"), eq("FAILURE"));
    }
}
