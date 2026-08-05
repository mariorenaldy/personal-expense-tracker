package com.mariorenaldy.expensetracker.service;

import com.mariorenaldy.expensetracker.dto.CategoryRequest;
import com.mariorenaldy.expensetracker.dto.CategoryResponse;
import com.mariorenaldy.expensetracker.entity.Category;
import com.mariorenaldy.expensetracker.exception.CategoryNotFoundException;
import com.mariorenaldy.expensetracker.exception.DuplicateCategoryException;
import com.mariorenaldy.expensetracker.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> list(Long userId) {
        return categoryRepository.findByUserIdOrderByNameAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse create(Long userId, CategoryRequest request) {
        if (categoryRepository.existsByUserIdAndName(userId, request.name())) {
            throw new DuplicateCategoryException("Category '" + request.name() + "' already exists");
        }

        Category category = new Category();
        category.setUserId(userId);
        category.setName(request.name());
        category.setColor(request.color());

        return toResponse(categoryRepository.save(category));
    }

    public CategoryResponse update(Long userId, Long id, CategoryRequest request) {
        Category category = getOwnedCategory(userId, id);

        boolean nameChanged = !category.getName().equals(request.name());
        if (nameChanged && categoryRepository.existsByUserIdAndName(userId, request.name())) {
            throw new DuplicateCategoryException("Category '" + request.name() + "' already exists");
        }

        category.setName(request.name());
        category.setColor(request.color());

        return toResponse(categoryRepository.save(category));
    }

    public void delete(Long userId, Long id) {
        Category category = getOwnedCategory(userId, id);
        categoryRepository.delete(category);
    }

    private Category getOwnedCategory(Long userId, Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category " + id + " not found"));

        if (!category.getUserId().equals(userId)) {
            throw new CategoryNotFoundException("Category " + id + " not found");
        }

        return category;
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getColor());
    }
}
