package com.evolution.dropfiledaemon.controller.api;

import com.evolution.dropfile.common.dto.ApiQuickShareAddRequestDTO;
import com.evolution.dropfile.common.dto.ApiQuickShareLsResponseDTO;
import com.evolution.dropfile.common.dto.ApiQuickShareRmResponseDTO;
import com.evolution.dropfiledaemon.facade.ApiQuickShareFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/quickshare")
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

    @DeleteMapping("/rm")
    public ApiQuickShareRmResponseDTO remove(@RequestBody Set<String> idCriteria) {
        return facade.removeByKeyStartWith(idCriteria);
    }

    @DeleteMapping("/rm-all")
    public void rmAll() {
        facade.removeAll();
    }
}
