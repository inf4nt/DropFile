package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.ApiConnectionsAccessGenerateRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsAccessInfoResponseDTO;
import com.evolution.dropfiledaemon.facade.ApiConnectionsAccessFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connections/access")
public class ApiConnectionsAccessRestController {

    private final ApiConnectionsAccessFacade apiFacade;

    @Autowired
    public ApiConnectionsAccessRestController(ApiConnectionsAccessFacade apiFacade) {
        this.apiFacade = apiFacade;
    }

    @PostMapping("/generate")
    public ApiConnectionsAccessInfoResponseDTO generate(@RequestBody ApiConnectionsAccessGenerateRequestDTO requestDTO) {
        return apiFacade.generate(requestDTO);
    }

    @GetMapping("/ls")
    public List<ApiConnectionsAccessInfoResponseDTO> ls() {
        return apiFacade.ls();
    }

    @DeleteMapping("/rm/{id}")
    public ResponseEntity<Void> rm(@PathVariable String id) {
        apiFacade.rm(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/rm-all")
    public ResponseEntity<Void> rmAll() {
        apiFacade.rmAll();
        return ResponseEntity.ok().build();
    }
}
