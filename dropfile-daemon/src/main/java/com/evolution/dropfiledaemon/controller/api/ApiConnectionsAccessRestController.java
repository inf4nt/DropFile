package com.evolution.dropfiledaemon.controller.api;

import com.evolution.dropfile.common.dto.ApiConnectionsAccessGenerateRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsAccessInfoResponseDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsAccessRmResponseDTO;
import com.evolution.dropfiledaemon.facade.ApiConnectionsAccessFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

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

    @DeleteMapping("/rm")
    public ApiConnectionsAccessRmResponseDTO rm(@RequestBody Set<String> idCriteria) {
        return apiFacade.rm(idCriteria);
    }

    @DeleteMapping("/rm-all")
    public void rmAll() {
        apiFacade.rmAll();
    }
}
