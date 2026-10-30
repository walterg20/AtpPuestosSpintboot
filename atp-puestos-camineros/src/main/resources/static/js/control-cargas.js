(function () {
  'use strict';

  function autofocusBarcode() {
    var input = document.getElementById('barCode');
    if (input) {
      try { input.focus(); } catch (e) { /* noop */ }
    }
  }

  function confirmarMarcado() {
    return window.confirm('Esta seguro que desea Marcar el formulario?');
  }

  // Exponer para los handlers inline de las plantillas.
  window.confirmarMarcado = confirmarMarcado;

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', autofocusBarcode);
  } else {
    autofocusBarcode();
  }
})();
