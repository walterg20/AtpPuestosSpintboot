/*
 * Pantalla de acceso — toggle de contraseña y autofocus (docs/design.md §8, T-202).
 *
 * JS puntual: vanilla, sin framework, sin bundler, sin CDN, sin `fetch`, sin `XMLHttpRequest`,
 * sin `eval` y sin validación ni lógica de negocio. Si la página no tiene los controles, el
 * script no hace nada (así puede enlazarse desde el layout compartido).
 *
 * El toggle NO lee la cookie `SUCURSAL`: es `HttpOnly` (D-LOG-07) y, además, no hace falta.
 */
(function () {
  'use strict';

  var ID_USUARIO = 'usuario';
  var ID_CLAVE = 'clave';
  var SELECTOR_BOTON = '[data-alternar-clave]';

  var ICONO_MOSTRAR = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>';
  var ICONO_OCULTAR = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>';
  var ETIQUETA_MOSTRAR = 'Mostrar contraseña';
  var ETIQUETA_OCULTAR = 'Ocultar contraseña';

  /** Muestra u oculta la clave y actualiza el botón. */
  function alternarVisibilidadDeLaClave() {
    var campo = document.getElementById(ID_CLAVE);
    var boton = document.querySelector(SELECTOR_BOTON);
    if (!campo || !boton) {
      return;
    }
    var visible = campo.getAttribute('type') === 'text';
    campo.setAttribute('type', visible ? 'password' : 'text');
    boton.innerHTML = visible ? ICONO_MOSTRAR : ICONO_OCULTAR;
    boton.setAttribute('aria-pressed', visible ? 'false' : 'true');
    boton.setAttribute('aria-label', visible ? ETIQUETA_MOSTRAR : ETIQUETA_OCULTAR);
  }

  /** Enfoca el campo de usuario al cargar la pantalla. */
  function enfocarElUsuario() {
    var campo = document.getElementById(ID_USUARIO);
    if (campo && typeof campo.focus === 'function') {
      campo.focus();
    }
  }

  function activar() {
    var boton = document.querySelector(SELECTOR_BOTON);
    if (boton) {
      boton.addEventListener('click', alternarVisibilidadDeLaClave);
    }
    enfocarElUsuario();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', activar);
  } else {
    activar();
  }
})();
