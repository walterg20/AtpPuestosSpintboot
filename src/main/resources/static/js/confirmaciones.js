/*
 * Confirmaciones de acciones destructivas — docs/design.md §8.
 *
 * JS puntual: reutiliza el diálogo nativo `confirm()` del navegador. Sin librerías, sin bundler,
 * sin CDN.
 *
 * Uso en la plantilla:
 *   <button type="submit" data-confirmar="¿Confirma la baja del producto?">Eliminar</button>
 *
 * El atributo `data-confirmar` lleva el texto del diálogo; si el usuario cancela, se cancela el
 * envío del formulario (no se emite el POST de baja) — R-PROD-14.
 */
(function () {
  'use strict';

  function confirmarEnvio(evento) {
    var boton = evento.currentTarget;
    var mensaje = boton.getAttribute('data-confirmar');
    if (!mensaje) {
      return;
    }
    if (!window.confirm(mensaje)) {
      evento.preventDefault();
    }
  }

  function activarConfirmaciones() {
    var botones = document.querySelectorAll('[data-confirmar]');
    Array.prototype.forEach.call(botones, function (boton) {
      boton.addEventListener('click', confirmarEnvio);
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', activarConfirmaciones);
  } else {
    activarConfirmaciones();
  }
})();
