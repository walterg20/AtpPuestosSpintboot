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

  var TEXTO_MOSTRAR = 'Mostrar';
  var TEXTO_OCULTAR = 'Ocultar';
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
    boton.textContent = visible ? TEXTO_MOSTRAR : TEXTO_OCULTAR;
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
