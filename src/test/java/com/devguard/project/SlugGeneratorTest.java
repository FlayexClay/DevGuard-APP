package com.devguard.project;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SlugGeneratorTest {

    @Test
    void nombre_simple_produce_slug_en_minusculas_con_guiones() {
        assertThat(SlugGenerator.from("Checkout Service")).isEqualTo("checkout-service");
    }

    @Test
    void tildes_y_diacriticos_se_eliminan() {
        assertThat(SlugGenerator.from("Cámara de Fotos")).isEqualTo("camara-de-fotos");
        assertThat(SlugGenerator.from("Ñoño API")).isEqualTo("nono-api");
    }

    @Test
    void simbolos_se_reemplazan_por_guion() {
        assertThat(SlugGenerator.from("Mi API v2!")).isEqualTo("mi-api-v2");
    }

    @Test
    void slug_no_empieza_ni_termina_con_guion() {
        String slug = SlugGenerator.from("--Mi Proyecto--");
        assertThat(slug).doesNotStartWith("-").doesNotEndWith("-");
    }

    @Test
    void slug_largo_se_trunca_a_50_caracteres() {
        String nombre = "Este es un nombre extremadamente largo que deberia superar cincuenta caracteres en el slug";
        String slug = SlugGenerator.from(nombre);
        assertThat(slug.length()).isLessThanOrEqualTo(50);
        assertThat(slug).doesNotEndWith("-");
    }

    @Test
    void slug_valido_cumple_formato_alfanumerico() {
        String slug = SlugGenerator.from("Mi Servicio 2025");
        assertThat(slug).matches("[a-z0-9][a-z0-9-]*");
    }

    @Test
    void nombre_que_produce_slug_muy_corto_lanza_excepcion() {
        // "ab" → slug "ab" → longitud 2 < 3
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SlugGenerator.from("ab"));
    }

    @Test
    void nombre_sin_alfanumericos_lanza_excepcion() {
        // "@@@" → slug "" → longitud 0 < 3
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SlugGenerator.from("@@@"));
    }
}