/*
 * Grilla anidada de vigencias — alta y baja de filas (docs/design.md §8, R-PROD-27).
 *
 * JS puntual y estrictamente mecánico:
 *   - `+ Fila` clona el <template id="plantilla-vigencia">, sustituye el marcador __INDEX__ por
 *     el índice de la nueva fila en los atributos name/id, sustituye el marcador __FILA__ por el
 *     número de fila (1-based) en el aria-label, vacía los valores de los inputs y la inserta al
 *     final del <tbody>.
 *   - `- Fila` quita la fila del DOM y renumera los name/id/aria-label de las filas siguientes para
 *     que los índices sigan siendo contiguos (Spring necesita 0..n-1) y para que el nombre
 *     accesible de cada control siga describiendo su posición (R-PROD-17, H1 del informe QA del
 *     2026-10-22).
 *
 * Prohibido (G6 / R-PROD-27): sin fetch, sin XMLHttpRequest, sin validación, sin cálculos de
 * negocio. La grilla se re-renderiza en el servidor después de cada escritura.
 */
(function () {
  'use strict';

  var MARCADOR = '__INDEX__';
  var MARCADOR_FILA = '__FILA__';
  var ID_PLANTILLA = 'plantilla-vigencia';
  var RE_NOMBRE = /vigencias\[\d+\]/;
  var RE_ID = /vigencias_\d+_/;
  var RE_FILA = /fila \d+/;

  function cuerpo(tabla) {
    return tabla.querySelector('tbody');
  }

  function filas(tabla) {
    return Array.prototype.slice.call(tabla.querySelectorAll('tbody tr[data-vigencia]'));
  }

  function campos(fila) {
    return Array.prototype.slice.call(fila.querySelectorAll('input, select, textarea'));
  }

  /** Renumera los name/id/aria-label de una fila al índice indicado. */
  function renumerarFila(fila, indice) {
    campos(fila).forEach(function (campo) {
      var nombre = campo.getAttribute('name');
      if (nombre) {
        campo.setAttribute('name', nombre.replace(RE_NOMBRE, 'vigencias[' + indice + ']'));
      }
      var id = campo.getAttribute('id');
      if (id) {
        campo.setAttribute('id', id.replace(RE_ID, 'vigencias_' + indice + '_'));
      }
      var nombreAccesible = campo.getAttribute('aria-label');
      if (nombreAccesible) {
        campo.setAttribute('aria-label', nombreAccesible.replace(RE_FILA, 'fila ' + (indice + 1)));
      }
    });
  }

  /** Renumera todas las filas para que los índices sean contiguos. */
  function renumerar(tabla) {
    filas(tabla).forEach(function (fila, indice) {
      renumerarFila(fila, indice);
    });
  }

  /** Agrega una fila vacía al final de la grilla. */
  function agregarFila(tabla) {
    var plantilla = document.getElementById(ID_PLANTILLA);
    if (!plantilla || !plantilla.content) {
      return;
    }
    var fragmento = plantilla.content.cloneNode(true);
    var fila = fragmento.querySelector('tr');
    if (!fila) {
      return;
    }
    var indice = filas(tabla).length;
    campos(fila).forEach(function (campo) {
      var nombre = campo.getAttribute('name');
      if (nombre) {
        campo.setAttribute('name', nombre.replace(MARCADOR, String(indice)));
      }
      var id = campo.getAttribute('id');
      if (id) {
        campo.setAttribute('id', id.replace(MARCADOR, String(indice)));
      }
      var nombreAccesible = campo.getAttribute('aria-label');
      if (nombreAccesible) {
        campo.setAttribute('aria-label', nombreAccesible.replace(MARCADOR_FILA, String(indice + 1)));
      }
      campo.value = '';
    });
    cuerpo(tabla).appendChild(fila);
  }

  /** Quita una fila y renumera las siguientes. */
  function quitarFila(fila, tabla) {
    if (fila && fila.parentNode) {
      fila.parentNode.removeChild(fila);
    }
    renumerar(tabla);
  }

  function activar() {
    var tabla = document.querySelector('[data-tabla-vigencias]');
    if (!tabla) {
      return;
    }

    var botonAgregar = document.querySelector('[data-agregar-vigencia]');
    if (botonAgregar) {
      botonAgregar.addEventListener('click', function () {
        agregarFila(tabla);
      });
    }

    // Delegación: las filas clonadas por el JS también quedan cubiertas.
    cuerpo(tabla).addEventListener('click', function (evento) {
      var boton = evento.target.closest('.js-quitar-vigencia');
      if (!boton) {
        return;
      }
      quitarFila(boton.closest('tr'), tabla);
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', activar);
  } else {
    activar();
  }
})();
