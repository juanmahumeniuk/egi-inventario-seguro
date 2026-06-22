package com.itu.egi.inventarioseguro.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping("/{path:[^\\.]*}")
    public String forwardSingleSegment() {
        return "forward:/index.html";
    }
}
