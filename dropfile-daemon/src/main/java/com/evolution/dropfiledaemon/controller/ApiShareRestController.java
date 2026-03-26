package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.ApiShareAddRequestDTO;
import com.evolution.dropfile.common.dto.ApiShareInfoResponseDTO;
import com.evolution.dropfiledaemon.facade.ApiShareFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/share")
public class ApiShareRestController {

    private final ApiShareFacade apiFacade;

    @PostMapping("/add")
    public ApiShareInfoResponseDTO add(@RequestBody ApiShareAddRequestDTO requestDTO) {
        return apiFacade.add(requestDTO);
    }

    @GetMapping("/ls")
    public List<ApiShareInfoResponseDTO> ls() {
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
