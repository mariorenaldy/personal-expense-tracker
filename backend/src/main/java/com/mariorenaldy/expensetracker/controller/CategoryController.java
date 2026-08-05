package com.mariorenaldy.expensetracker.controller;

import com.mariorenaldy.expensetracker.dto.CategoryRequest;
import com.mariorenaldy.expensetracker.dto.CategoryResponse;
import com.mariorenaldy.expensetracker.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private static final Long TEMP_USER_ID = 1L;
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService){
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> list(){
        return categoryService.list(TEMP_USER_ID);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request){
        return categoryService.create(TEMP_USER_ID, request);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request){
        return categoryService.update(TEMP_USER_ID, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id){
        categoryService.delete(TEMP_USER_ID, id);
    }
}
