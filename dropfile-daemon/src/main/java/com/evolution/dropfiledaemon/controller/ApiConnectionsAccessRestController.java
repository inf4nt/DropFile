package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.ApiConnectionsAccessGenerateRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsAccessInfoResponseDTO;
import com.evolution.dropfiledaemon.facade.ApiConnectionsAccessFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/connections/access")
public class ApiConnectionsAccessRestController {

    private final ApiConnectionsAccessFacade apiFacade;

    @PostMapping("/generate")
    public ApiConnectionsAccessInfoResponseDTO generate(@RequestBody ApiConnectionsAccessGenerateRequestDTO requestDTO) {
        return apiFacade.generate(requestDTO);
    }

    @GetMapping("/ls")
    public List<ApiConnectionsAccessInfoResponseDTO> ls() {
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
