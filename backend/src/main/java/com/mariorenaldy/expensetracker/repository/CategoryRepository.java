package com.mariorenaldy.expensetracker.repository;

import com.mariorenaldy.expensetracker.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIdOrderByNameAsc(Long userId);

    boolean existsByUserIdAndName(Long userId, String name);

}