    package com.lax.lms.repository;

    import com.lax.lms.LibraryUser;
    import org.springframework.data.jpa.repository.JpaRepository;

    import java.util.Optional;

    public interface LibraryUserRepository
            extends JpaRepository<LibraryUser, Long> {

        Optional<LibraryUser> findByEmailIgnoreCase(String email);

        boolean existsByEmailIgnoreCase(String email);

        long count();
    }           