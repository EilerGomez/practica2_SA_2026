/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.app_oidc.controllers;

/**
 *
 * @author eiler
 */

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
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
    public String privada(@AuthenticationPrincipal OidcUser user, Model model) {
        model.addAttribute("nombre", user.getFullName());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("subject", user.getSubject());
        model.addAttribute("claims", user.getClaims());
        model.addAttribute("idToken", user.getIdToken().getTokenValue());
        return "privada";
    }
}
