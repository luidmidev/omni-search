package io.github.luidmidev.omnisearch.jpa;

import io.github.luidmidev.omnisearch.core.OmniSearchOptions;
import cz.jirutka.rsql.parser.RSQLParser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JpaOmniSearchTest {

    private EntityManagerFactory emf;
    private EntityManager em;
    private JpaOmniSearch omniSearch;

    @BeforeAll
    void init() {
        emf = Persistence.createEntityManagerFactory("test-pu");
    }

    @BeforeEach
    void setUp() {
        em = emf.createEntityManager();
        em.getTransaction().begin();

        em.persist(User.builder()
                .name("Alice")
                .email("alice@example.com")
                .active(true)
                .level(User.Level.HIGH)
                .roles(Set.of(User.Role.USER, User.Role.ADMIN))
                .contacts(Set.of(
                        Contacts.builder()
                                .firstName("Contact1")
                                .lastName("Last1")
                                .build(),
                        Contacts.builder()
                                .firstName("Contact2")
                                .lastName("Last2")
                                .build()
                ))
                .build()
        );

        em.persist(User.builder()
                .name("Bob")
                .email("bob@example.com")
                .active(false)
                .level(User.Level.MEDIUM)
                .roles(Set.of(User.Role.GUEST))
                .build()
        );

        em.persist(User.builder()
                .name("Dave")
                .email("charlie@example.net")
                .active(true)
                .level(User.Level.LOW)
                .roles(Set.of(User.Role.USER))
                .build()
        );

        em.getTransaction().commit();

        omniSearch = new JpaOmniSearch(em);
    }

    @AfterEach
    void tearDown() {
        em.getTransaction().begin();
        var users = em.createQuery("SELECT u FROM User u", User.class).getResultList();
        for (User user : users) {
            em.remove(user);
        }
        em.getTransaction().commit();
        em.close();
    }

    @AfterAll
    void close() {
        emf.close();
    }

    @Test
    void testSearchByName() {
        var options = new OmniSearchOptions()
                .search("Alice");

        List<User> result = omniSearch.search(User.class, options);
        assertEquals(1, result.size());
        assertEquals("Alice", result.getFirst().getName());
    }

    @Test
    void testSearchByEmailDomain() {
        var options = new OmniSearchOptions()
                .search("example.com");

        List<User> result = omniSearch.search(User.class, options);
        assertEquals(2, result.size());
    }

    @Test
    void testSearchBooleanTrue() {
        var options = new OmniSearchOptions()
                .search("true");

        List<User> result = omniSearch.search(User.class, options);
        assertEquals(2, result.size());
    }

    @Test
    void testSearchEnumLevel() {
        var options = new OmniSearchOptions()
                .search("HIGH");

        List<User> result = omniSearch.search(User.class, options);
        assertEquals(1, result.size());
        assertEquals(User.Level.HIGH, result.getFirst().getLevel());
    }

    @Test
    void testSearchContactFirstName() {

        var options = new OmniSearchOptions()
                .search("Contact1")
                .joins("contacts");

        List<User> result = omniSearch.search(User.class, options);
        assertEquals(1, result.size());
        assertTrue(result.getFirst().getContacts().stream().anyMatch(contact -> "Contact1".equals(contact.getFirstName())));
    }


    @Test
    void testSearchByRole() {
        var options = new OmniSearchOptions()
                .search("USER");

        List<User> result = omniSearch.search(User.class, options);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(user -> user.getRoles().contains(User.Role.USER)));
    }

    @Test
    void testSearchFilterNameAndEmail() {

        var conditions = new RSQLParser().parse("name==alice;email==*example.com*");
        var options = new OmniSearchOptions()
                .conditions(conditions);

        List<User> result = omniSearch.search(User.class, options);
        assertEquals(1, result.size());
        assertEquals("Alice", result.getFirst().getName());
        assertTrue(result.getFirst().getEmail().contains("example.com"));
    }
}
