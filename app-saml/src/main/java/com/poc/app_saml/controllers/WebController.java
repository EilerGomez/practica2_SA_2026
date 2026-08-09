/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.app_saml.controllers;

/**
 *
 * @author eiler
 */

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/publica")
    public String publica() {
        return "publica";
    }

    @GetMapping("/privada")
    public String privada(@AuthenticationPrincipal Saml2AuthenticatedPrincipal user, Model model) {
        model.addAttribute("nameId", user.getName());
        model.addAttribute("atributos", user.getAttributes());
        return "privada";
    }
}
