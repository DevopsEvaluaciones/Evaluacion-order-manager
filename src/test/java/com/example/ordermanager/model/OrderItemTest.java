
package com.example.ordermanager.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderItem - Pruebas unitarias")
class OrderItemTest {

    @Test
    @DisplayName("Debe crear un ítem con datos válidos")
    void shouldCreateItemWithValidData() {
        OrderItem item = new OrderItem(
                " Laptop ",
                2,
                new BigDecimal("100.00")
        );

        assertEquals("Laptop", item.getProductName());
        assertEquals(2, item.getQuantity());
        assertEquals(new BigDecimal("100.00"), item.getUnitPrice());
    }

    @Test
    @DisplayName("Debe rechazar un nombre nulo")
    void shouldRejectNullName() {
        assertThrows(IllegalArgumentException.class,
                () -> new OrderItem(null, 1, new BigDecimal("10.00")));
    }

    @Test
    @DisplayName("Debe rechazar un nombre vacío")
    void shouldRejectBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> new OrderItem("   ", 1, new BigDecimal("10.00")));
    }

    @Test
    @DisplayName("Debe rechazar una cantidad igual a cero")
    void shouldRejectZeroQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> new OrderItem("Mouse", 0, new BigDecimal("10.00")));
    }

    @Test
    @DisplayName("Debe rechazar una cantidad negativa")
    void shouldRejectNegativeQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> new OrderItem("Mouse", -1, new BigDecimal("10.00")));
    }

    @Test
    @DisplayName("Debe rechazar un precio nulo")
    void shouldRejectNullPrice() {
        assertThrows(IllegalArgumentException.class,
                () -> new OrderItem("Mouse", 1, null));
    }

    @Test
    @DisplayName("Debe rechazar un precio negativo")
    void shouldRejectNegativePrice() {
        assertThrows(IllegalArgumentException.class,
                () -> new OrderItem("Mouse", 1, new BigDecimal("-1.00")));
    }

    @Test
    @DisplayName("Debe aceptar un precio igual a cero")
    void shouldAcceptZeroPrice() {
        OrderItem item = new OrderItem("Mouse", 1, BigDecimal.ZERO);

        assertEquals(BigDecimal.ZERO, item.getUnitPrice());
        assertEquals(BigDecimal.ZERO, item.getSubtotal());
    }

    @Test
    @DisplayName("Debe calcular el subtotal")
    void shouldCalculateSubtotal() {
        OrderItem item = new OrderItem(
                "Mouse",
                3,
                new BigDecimal("10.50")
        );

        assertEquals(new BigDecimal("31.50"), item.getSubtotal());
    }

    @Test
    @DisplayName("Debe permitir crear un ítem con el constructor vacío")
    void shouldCreateItemWithEmptyConstructor() {
        OrderItem item = new OrderItem();

        assertNull(item.getProductName());
        assertEquals(0, item.getQuantity());
        assertNull(item.getUnitPrice());
    }

    @Test
    @DisplayName("Debe actualizar los atributos con los setters")
    void shouldUpdateAttributesWithSetters() {
        OrderItem item = new OrderItem();

        item.setProductName("Teclado");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("15.00"));

        assertEquals("Teclado", item.getProductName());
        assertEquals(2, item.getQuantity());
        assertEquals(new BigDecimal("15.00"), item.getUnitPrice());
    }

    @Test
    @DisplayName("Dos ítems iguales deben ser equivalentes")
    void shouldCompareEqualItems() {
        OrderItem first = new OrderItem(
                "Mouse", 2, new BigDecimal("10.00"));
        OrderItem second = new OrderItem(
                "Mouse", 2, new BigDecimal("10.00"));

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    @DisplayName("Debe comparar correctamente ítems diferentes")
    void shouldCompareDifferentItems() {
        OrderItem first = new OrderItem(
                "Mouse", 2, new BigDecimal("10.00"));
        OrderItem second = new OrderItem(
                "Teclado", 2, new BigDecimal("10.00"));

        assertNotEquals(first, second);
    }

    @Test
    @DisplayName("Debe reconocer el mismo objeto")
    void shouldRecognizeSameObject() {
        OrderItem item = new OrderItem(
                "Mouse", 1, new BigDecimal("10.00"));

        assertEquals(item, item);
    }

    @Test
    @DisplayName("Debe rechazar la comparación con null")
    void shouldNotEqualNull() {
        OrderItem item = new OrderItem(
                "Mouse", 1, new BigDecimal("10.00"));

        assertNotEquals(null, item);
    }

    @Test
    @DisplayName("Debe rechazar la comparación con otra clase")
    void shouldNotEqualDifferentClass() {
        OrderItem item = new OrderItem(
                "Mouse", 1, new BigDecimal("10.00"));

        assertNotEquals("Mouse", item);
    }
}
