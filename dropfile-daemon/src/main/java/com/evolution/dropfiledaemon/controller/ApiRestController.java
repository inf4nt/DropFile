package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfiledaemon.facade.ApiFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiRestController {

    private final ApiFacade apiFacade;

    public ApiRestController(ApiFacade apiFacade) {
        this.apiFacade = apiFacade;
    }

    @PostMapping("/share/add")
    public ApiShareInfoResponseDTO addShareFile(@RequestBody ApiShareAddRequestDTO requestDTO) {
        return apiFacade.shareAdd(requestDTO);
    }

    @GetMapping("/share/ls")
    public List<ApiShareInfoResponseDTO> getShareFiles() {
        return apiFacade.shareLs();
    }

    @DeleteMapping("/share/rm/{id}")
    public ResponseEntity<ApiShareInfoResponseDTO> rmShareFile(@PathVariable String id) {
        ApiShareInfoResponseDTO responseDTO = apiFacade.shareRm(id);
        if (responseDTO != null) {
            return ResponseEntity.ok(responseDTO);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/share/rm-all")
    public ResponseEntity<Void> rmAllShareFiles() {
        apiFacade.shareRmAll();
        return ResponseEntity.ok().build();
    }
}
