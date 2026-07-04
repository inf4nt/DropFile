package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.ApiQuickShareAddRequestDTO;
import com.evolution.dropfile.common.dto.ApiQuickShareLsResponseDTO;
import com.evolution.dropfiledaemon.facade.ApiQuickShareFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/quick-share")
public class ApiQuickShareRestController {

    private final ApiQuickShareFacade facade;

    @PostMapping("/add")
    public ApiQuickShareLsResponseDTO add(@RequestBody ApiQuickShareAddRequestDTO requestDTO) {
        return facade.add(requestDTO);
    }

    @GetMapping("/ls")
    public List<ApiQuickShareLsResponseDTO> getAll() {
        return facade.ls();
    }

    @GetMapping("/ls/{id}")
    public ApiQuickShareLsResponseDTO getById(@PathVariable String id) {
        return facade.ls(id);
    }

    @DeleteMapping("/rm/{id}")
    public void removeById(@PathVariable String id) {
        facade.removeByKeyStartWith(id);
    }

    @DeleteMapping("/rm-all")
    public void rmAll() {
        facade.removeAll();
    }
}
