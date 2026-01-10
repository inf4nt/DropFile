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
    public ApiConnectionsAccessInfoResponseDTO connectionsAccessGenerate(@RequestBody ApiConnectionsAccessGenerateRequestDTO requestDTO) {
        return apiFacade.generate(requestDTO);
    }

    @GetMapping("/ls")
    public List<ApiConnectionsAccessInfoResponseDTO> connectionsAccessLs() {
        return apiFacade.ls();
    }

    @DeleteMapping("/rm/{id}")
    public ResponseEntity<Void> connectionsAccessRm(@PathVariable String id) {
        boolean result = apiFacade.rm(id);
        if (result) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/rm-all")
    public ResponseEntity<Void> connectionsAccessRmAll() {
        apiFacade.rmAll();
        return ResponseEntity.ok().build();
    }
}
