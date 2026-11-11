package com.perkmanager.perk_manager.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void homepageRenders() {
        assertThat(mvc.get().uri("/"))
            .hasStatusOk()
            .hasViewName("index")
            .bodyText().contains("Perk Manager");
    }

    @Test
    void homepageMarksHomeAsCurrentNavItem() {
        assertThat(mvc.get().uri("/"))
            .hasStatusOk()
            .bodyText()
            .contains("aria-label=\"Primary\"")
            .contains("aria-current=\"page\">Home</a>")
            .doesNotContain("aria-current=\"page\">Memberships</a>");
    }

    @Test
    void homepageLoadsVendoredStyles() {
        assertThat(mvc.get().uri("/"))
            .hasStatusOk()
            .bodyText()
            .contains("/vendor/tailwindcss-browser.js")
            .contains("/vendor/basecoat/basecoat-sera.cdn.min.css")
            .contains("/vendor/basecoat/all.min.js");
    }
}
