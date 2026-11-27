
package com.example.ordermanager.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Order - Pruebas unitarias")
class OrderTest {

    @Test
    @DisplayName("Debe crear un pedido válido")
    void shouldCreateValidOrder() {
        Order order = new Order("  Juan Pérez  ");

        assertEquals("Juan Pérez", order.getCustomerName());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(BigDecimal.ZERO, order.getTotalAmount());
        assertTrue(order.getItems().isEmpty());
    }

    @Test
    @DisplayName("Debe rechazar un nombre nulo")
    void shouldRejectNullCustomerName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Order(null));
    }

    @Test
    @DisplayName("Debe rechazar un nombre vacío")
    void shouldRejectBlankCustomerName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Order("   "));
    }

    @Test
    @DisplayName("Debe crear un pedido con constructor vacío")
    void shouldCreateOrderWithEmptyConstructor() {
        Order order = new Order();

        assertNull(order.getCustomerName());
        assertNull(order.getStatus());
        assertNull(order.getTotalAmount());
        assertNotNull(order.getItems());
        assertTrue(order.getItems().isEmpty());
    }

    @Test
    @DisplayName("Debe agregar un producto y calcular el total")
    void shouldAddItemAndCalculateTotal() {
        Order order = new Order("Juan");
        OrderItem item = new OrderItem(
                "Mouse", 2, new BigDecimal("10.00"));

        order.addItem(item);

        assertEquals(1, order.getItems().size());
        assertEquals(new BigDecimal("20.00"), order.getTotalAmount());
    }

    @Test
    @DisplayName("Debe sumar los subtotales de varios productos")
    void shouldCalculateTotalOfMultipleItems() {
        Order order = new Order("Juan");

        order.addItem(new OrderItem(
                "Mouse", 2, new BigDecimal("10.00")));
        order.addItem(new OrderItem(
                "Teclado", 1, new BigDecimal("25.50")));

        assertEquals(new BigDecimal("45.50"), order.getTotalAmount());
        assertEquals(2, order.getItems().size());
    }

    @Test
    @DisplayName("Debe permitir modificar un pedido pendiente")
    void shouldAllowModificationWhenPending() {
        Order order = new Order("Juan");

        assertTrue(order.canBeModified());
    }

    @Test
    @DisplayName("No debe permitir modificar un pedido confirmado")
    void shouldNotAllowModificationWhenConfirmed() {
        Order order = new Order("Juan");
        order.setStatus(OrderStatus.CONFIRMED);

        assertFalse(order.canBeModified());
    }

    @Test
    @DisplayName("No debe permitir modificar un pedido con estado nulo")
    void shouldNotAllowModificationWhenStatusIsNull() {
        Order order = new Order("Juan");
        order.setStatus(null);

        assertFalse(order.canBeModified());
    }

    @Test
    @DisplayName("Debe inicializar fecha, estado y total al crear")
    void shouldInitializeFieldsOnCreate() {
        Order order = new Order();
        order.onCreate();

        assertNotNull(order.getCreatedAt());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(BigDecimal.ZERO, order.getTotalAmount());
    }

    @Test
    @DisplayName("Debe conservar el estado y total existentes al crear")
    void shouldPreserveExistingStatusAndTotalOnCreate() {
        Order order = new Order();
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotalAmount(new BigDecimal("50.00"));

        order.onCreate();

        assertNotNull(order.getCreatedAt());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(new BigDecimal("50.00"), order.getTotalAmount());
    }

    @Test
    @DisplayName("Debe asignar y recuperar los atributos")
    void shouldSetAndGetAttributes() {
        Order order = new Order();
        LocalDateTime date = LocalDateTime.of(2026, 1, 1, 12, 0);
        List<OrderItem> items = new ArrayList<>();

        order.setId(10L);
        order.setCustomerName("Ana");
        order.setStatus(OrderStatus.SHIPPED);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setCreatedAt(date);
        order.setItems(items);

        assertEquals(10L, order.getId());
        assertEquals("Ana", order.getCustomerName());
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        assertEquals(new BigDecimal("100.00"), order.getTotalAmount());
        assertEquals(date, order.getCreatedAt());
        assertSame(items, order.getItems());
    }

    @Test
    @DisplayName("Debe considerar iguales dos pedidos con el mismo ID")
    void shouldCompareOrdersWithSameId() {
        Order first = new Order("Juan");
        Order second = new Order("Ana");

        first.setId(1L);
        second.setId(1L);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    @DisplayName("Debe considerar diferentes pedidos con distintos ID")
    void shouldCompareOrdersWithDifferentIds() {
        Order first = new Order("Juan");
        Order second = new Order("Ana");

        first.setId(1L);
        second.setId(2L);

        assertNotEquals(first, second);
    }

    @Test
    @DisplayName("Debe reconocer el mismo objeto")
    void shouldRecognizeSameObject() {
        Order order = new Order("Juan");

        assertEquals(order, order);
    }

    @Test
    @DisplayName("No debe considerar igual un pedido con null")
    void shouldNotEqualNull() {
        Order order = new Order("Juan");

        assertNotEquals(null, order);
    }

    @Test
    @DisplayName("No debe considerar igual un objeto de otra clase")
    void shouldNotEqualDifferentClass() {
        Order order = new Order("Juan");

        assertNotEquals("Juan", order);
    }
}
