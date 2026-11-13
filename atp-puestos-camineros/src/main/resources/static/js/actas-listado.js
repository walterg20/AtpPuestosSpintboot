/*
 * Actas Listado — JS puntual (filtros, reseteo, impresión). Vanilla.
 */
(function () {
  'use strict';

  function byId(id) { return document.getElementById(id); }

  function activarFiltros() {
    var btnReset = byId('btnReset');
    if (btnReset) {
      btnReset.addEventListener('click', function () {
        var form = btnReset.closest('form');
        if (form) {
          form.reset();
        }
      });
    }
    var btnExportar = byId('btnExportar');
    if (btnExportar) {
      btnExportar.addEventListener('click', function () {
        window.print();
      });
    }
  }

  function activarImpresion() {
    // delegado: botones con data-acta-id
  }

  function activar() {
    activarFiltros();
    activarImpresion();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', activar);
  } else {
    activar();
  }
})();