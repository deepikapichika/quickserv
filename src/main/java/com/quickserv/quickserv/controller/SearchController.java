package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.dto.search.ServiceSearchResultDto;
import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.service.CategoryService;
import com.quickserv.quickserv.service.ServiceService;
import com.quickserv.quickserv.service.SubcategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/services")
public class SearchController {

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SubcategoryService subcategoryService;

    @GetMapping(value = "/search", produces = MediaType.TEXT_HTML_VALUE)
    public String searchServices(@RequestParam(required = false) String location,
                                 @RequestParam(required = false) Long categoryId,
                                 @RequestParam(name = "subcategory_id", required = false) Long subcategoryId,
                                 @RequestParam(required = false) BigDecimal minPrice,
                                 @RequestParam(required = false) BigDecimal maxPrice,
                                 @RequestParam(required = false) Double minRating,
                                 Model model) {

        List<ServiceSearchResultDto> searchResults = serviceService.searchDiscovery(
                location,
                categoryId,
                subcategoryId,
                minPrice,
                maxPrice,
                minRating
        );
        List<Category> categories = categoryService.getAllCategories();

        model.addAttribute("searchResults", searchResults);
        model.addAttribute("categories", categories);
        model.addAttribute("subcategories", subcategoryService.getAllSubcategories());
        model.addAttribute("selectedLocation", location);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedSubcategoryId", subcategoryId);
        model.addAttribute("selectedMinPrice", minPrice);
        model.addAttribute("selectedMaxPrice", maxPrice);
        model.addAttribute("minRating", minRating);

        return "search-results";
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<ServiceSearchResultDto> searchServicesApi(@RequestParam(required = false) String location,
                                                          @RequestParam(required = false) Long categoryId,
                                                          @RequestParam(name = "subcategory_id", required = false) Long subcategoryId,
                                                          @RequestParam(required = false) BigDecimal minPrice,
                                                          @RequestParam(required = false) BigDecimal maxPrice,
                                                          @RequestParam(required = false) Double minRating) {
        return serviceService.searchDiscovery(location, categoryId, subcategoryId, minPrice, maxPrice, minRating);
    }

    @GetMapping("/quick-search")
    @ResponseBody
    public List<ServiceSearchResultDto> quickSearch(@RequestParam(required = false) String query,
                                                    @RequestParam(required = false) Long categoryId,
                                                    @RequestParam(name = "subcategory_id", required = false) Long subcategoryId,
                                                    @RequestParam(required = false) BigDecimal maxPrice) {
        return serviceService.searchDiscovery(query, categoryId, subcategoryId, null, maxPrice, null);
    }
}
