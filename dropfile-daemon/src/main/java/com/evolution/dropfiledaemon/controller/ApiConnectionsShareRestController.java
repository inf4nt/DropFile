package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.ApiConnectionsShareAddRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
import com.evolution.dropfiledaemon.facade.ApiConnectionsShareFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/connections/share")
public class ApiConnectionsShareRestController {

    private final ApiConnectionsShareFacade apiFacade;

    @PostMapping("/add")
    public ApiConnectionsShareLsResponseDTO add(@RequestBody ApiConnectionsShareAddRequestDTO requestDTO) {
        return apiFacade.add(requestDTO);
    }

    @GetMapping("/ls")
    public List<ApiConnectionsShareLsResponseDTO> ls() {
        return apiFacade.ls();
    }

    @DeleteMapping("/rm/{id}")
    public void rm(@PathVariable String id) {
        apiFacade.rm(id);
    }

    @DeleteMapping("/rm-all")
    public void rmAll() {
        apiFacade.rmAll();
    }
}
