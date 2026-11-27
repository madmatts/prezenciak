package pl.wishka.user.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.wishka.user.api.AuthProvider;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select u from User u join u.providers p where p.provider = :provider and p.subject = :subject")
    Optional<User> findByProvider(AuthProvider provider, String subject);
}
