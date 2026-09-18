package com.campuscycle.dao;

import java.util.List;
import java.util.Optional;

/**
 * Generic DAO Interface demonstrating standard CRUD operations and Java Generics.
 *
 * @param <T>  Entity Type
 * @param <ID> Key Type
 */
public interface GenericDao<T, ID> {
    // Create
    T save(T entity);

    // Read
    Optional<T> findById(ID id);
    List<T> findAll();

    // Update
    boolean update(T entity);

    // Delete
    boolean delete(ID id);
}
