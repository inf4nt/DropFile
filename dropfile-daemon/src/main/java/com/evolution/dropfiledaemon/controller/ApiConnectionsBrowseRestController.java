package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.ApiConnectionsBrowseGetRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsBrowseGetResponseDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsBrowseLsRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsBrowseLsResponseDTO;
import com.evolution.dropfiledaemon.facade.ApiConnectionsBrowseFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/connections/browse")
public class ApiConnectionsBrowseRestController {

    private final ApiConnectionsBrowseFacade apiFacade;

    @PostMapping("/ls")
    public List<ApiConnectionsBrowseLsResponseDTO> ls(@RequestBody ApiConnectionsBrowseLsRequestDTO requestDTO) {
        return apiFacade.ls(requestDTO);
    }

    @PostMapping("/get")
    public ApiConnectionsBrowseGetResponseDTO get(@RequestBody ApiConnectionsBrowseGetRequestDTO requestDTO) {
        return apiFacade.get(requestDTO);
    }
}
