package fin.repository;

import java.util.List;
import java.util.Optional;

/**
 * Base repository interface defining common CRUD operations.
 * @param <T> Entity type
 * @param <ID> ID type
 */
public interface BaseRepository<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void delete(T entity);
    void deleteById(ID id);
    boolean exists(ID id);
}
