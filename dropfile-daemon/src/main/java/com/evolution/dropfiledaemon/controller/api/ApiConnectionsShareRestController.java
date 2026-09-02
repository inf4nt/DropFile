package com.evolution.dropfiledaemon.controller.api;

import com.evolution.dropfile.common.dto.ApiBatchOperationResult;
import com.evolution.dropfile.common.dto.ApiConnectionsShareAddRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
import com.evolution.dropfiledaemon.facade.ApiConnectionsShareFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
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

    @DeleteMapping("/rm")
    public ApiBatchOperationResult rm(@RequestBody Collection<String> idCriteria) {
        return apiFacade.rm(idCriteria);
    }

    @DeleteMapping("/rm-all")
    public void rmAll() {
        apiFacade.rmAll();
    }
}
