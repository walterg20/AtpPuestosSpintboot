/*
 * Actas (modulo 09 "Formularios Acta") — JS puntual de la pantalla de alta.
 *
 * JS vanilla, sin framework, sin bundler y sin recursos externos. No hace peticiones HTTP ni
 * evalúa código dinámico: la validación y los importes los calcula `ActaService` (G6, D-ACTA-11).
 *
 * Qué hace:
 *   - `+` clona la fila-plantilla (`#acta-producto-plantilla`), sustituye __INDEX__ / __FILA__ y la
 *     agrega al final del `<tbody>` de la grilla de productos.
 *   - `−` quita la fila y renumera los índices para que el binding siga siendo `productos[i].*`
 *     (Spring necesita 0..n-1) y para que el nombre accesible de cada control siga describiendo
 *     su posición (R-ACTA-055).
 *   - Al elegir un producto se actualiza la descripción y, si `ProdTipCal = 3`, la cantidad se
 *     fuerza a 1 y queda en sólo lectura (R-ACTA-021).
 *   - La confirmación del alta usa el diálogo nativo `confirm()` con el texto verbatim del legacy.
 *   - Enfoca el primer control del formulario.
 */
(function () {
  'use strict';

  var ID_PLANTILLA = 'acta-producto-plantilla';
  var ID_CUERPO = 'acta-productos-body';
  var MARCADOR_INDICE = '__INDEX__';
  var MARCADOR_FILA = '__FILA__';
  var RE_NOMBRE = /productos\[\d+\]/;
  var MENSAJE_CONFIRMACION = '¿Confirma que desea enviar el Formulario de Acta?';

  // ------------------------------------------------------------- Utilidades

  function todos(selector, raiz) {
    return Array.prototype.slice.call((raiz || document).querySelectorAll(selector));
  }

  /** Traduce el nombre del control al id estable de la fila: productos[0].prodCod -> productos_0_prodCod */
  function idDe(nombre) {
    return nombre.replace(/productos\[(\d+)\]\.(\w+)/, 'productos_$1_$2');
  }

  function controles(fila) {
    return todos('input, select, textarea', fila);
  }

  // -------------------------------------------------------- Grilla de productos

  /** Renumera los controles de la fila para que el índice sea el de su posición. */
  function renumerarFila(fila, indice) {
    controles(fila).forEach(function (control) {
      var nombre = control.getAttribute('name');
      if (!nombre) {
        return;
      }
      var nuevo = nombre.replace(RE_NOMBRE, 'productos[' + indice + ']');
      control.setAttribute('name', nuevo);

      var idAnterior = control.getAttribute('id');
      var id = idDe(nuevo);
      control.setAttribute('id', id);

      var etiqueta = idAnterior ? fila.querySelector('label[for="' + idAnterior + '"]') : null;
      if (etiqueta) {
        etiqueta.setAttribute('for', id);
        var rotulo = etiqueta.getAttribute('data-rotulo');
        if (rotulo) {
          etiqueta.textContent = rotulo + ' fila ' + (indice + 1);
        }
      }
    });

    var boton = fila.querySelector('.js-quitar-producto');
    if (boton) {
      boton.setAttribute('data-indice', String(indice));
      boton.setAttribute('aria-label', 'Eliminar fila ' + (indice + 1));
    }
  }

  /** Renumera todas las filas de la grilla. */
  function renumerar(cuerpo) {
    todos('tr[data-acta-producto]', cuerpo).forEach(function (fila, indice) {
      renumerarFila(fila, indice);
    });
  }

  /** Agrega una fila vacía al final de la grilla. */
  function agregarFila(cuerpo) {
    var plantilla = document.getElementById(ID_PLANTILLA);
    if (!plantilla || !plantilla.content) {
      return;
    }
    var fragmento = plantilla.content.cloneNode(true);
    var fila = fragmento.querySelector('tr');
    if (!fila) {
      return;
    }
    var indice = todos('tr[data-acta-producto]', cuerpo).length;
    controles(fila).forEach(function (control) {
      var nombre = control.getAttribute('name');
      if (nombre) {
        control.setAttribute('name', nombre.replace(MARCADOR_INDICE, String(indice)));
      }
      var id = control.getAttribute('id');
      if (id) {
        control.setAttribute('id', id.replace(MARCADOR_INDICE, String(indice)));
      }
      control.value = '';
    });
    todos('label[data-rotulo]', fila).forEach(function (etiqueta) {
      var id = etiqueta.getAttribute('for');
      if (id) {
        etiqueta.setAttribute('for', id.replace(MARCADOR_INDICE, String(indice)));
      }
      etiqueta.textContent = etiqueta.getAttribute('data-rotulo') + ' fila ' + (indice + 1);
    });
    var boton = fila.querySelector('.js-quitar-producto');
    if (boton) {
      boton.setAttribute('data-indice', String(indice));
      boton.setAttribute('aria-label', 'Eliminar fila ' + (indice + 1));
    }
    cuerpo.appendChild(fila);
    renumerar(cuerpo);
  }

  /** Quita una fila y renumera las siguientes. */
  function quitarFila(fila, cuerpo) {
    if (fila && fila.parentNode) {
      fila.parentNode.removeChild(fila);
    }
    renumerar(cuerpo);
  }

  /**
   * Sincroniza la fila con el producto elegido: actualiza la descripción y, cuando
   * `ProdTipCal = 3`, fuerza la cantidad a 1 y la deja en sólo lectura (R-ACTA-021).
   */
  function sincronizarFila(fila) {
    var select = fila.querySelector('[data-producto]');
    if (!select) {
      return;
    }
    var opcion = select.selectedIndex >= 0 ? select.options[select.selectedIndex] : null;
    var descripcion = fila.querySelector('[data-descripcion]');
    if (descripcion) {
      descripcion.textContent = opcion ? (opcion.getAttribute('data-descripcion') || '') : '';
    }
    var cantidad = fila.querySelector('[data-cantidad]');
    if (!cantidad) {
      return;
    }
    var tipoCalculo = opcion ? opcion.getAttribute('data-tipcal') : null;
    if (tipoCalculo === '3') {
      cantidad.value = '1';
      cantidad.setAttribute('readonly', 'readonly');
      cantidad.setAttribute('aria-disabled', 'true');
    } else {
      cantidad.removeAttribute('readonly');
      cantidad.removeAttribute('aria-disabled');
    }
  }

  function activarGrilla() {
    var cuerpo = document.getElementById(ID_CUERPO);
    if (!cuerpo) {
      return;
    }

    var botonAgregar = document.getElementById('acta-agregar-producto');
    if (botonAgregar) {
      botonAgregar.addEventListener('click', function () {
        agregarFila(cuerpo);
      });
    }

    // Delegación: las filas clonadas por el JS también quedan cubiertas.
    cuerpo.addEventListener('click', function (evento) {
      var boton = evento.target.closest('.js-quitar-producto');
      if (!boton) {
        return;
      }
      quitarFila(boton.closest('tr'), cuerpo);
    });

    cuerpo.addEventListener('change', function (evento) {
      var select = evento.target.closest('[data-producto]');
      if (!select) {
        return;
      }
      sincronizarFila(select.closest('tr'));
    });

    // Las filas ya renderizadas por el servidor quedan alineadas con su producto.
    todos('tr[data-acta-producto]', cuerpo).forEach(function (fila) {
      var select = fila.querySelector('[data-producto]');
      if (select && select.selectedIndex > 0) {
        sincronizarFila(fila);
      }
    });
  }

  // --------------------------------------------------- Confirmación del alta

  function activarConfirmacion() {
    var botones = todos('[data-confirmar-acta]');
    botones.forEach(function (boton) {
      boton.addEventListener('click', function (evento) {
        var mensaje = boton.getAttribute('data-confirmar-acta') || MENSAJE_CONFIRMACION;
        if (!window.confirm(mensaje)) {
          evento.preventDefault();
        }
      });
    });
  }

  // -------------------------------------------------------------- Autofocus

  function enfocarPrimerCampo() {
    var formulario = document.querySelector('.atp-actas__form');
    if (!formulario) {
      return;
    }
    if (document.activeElement && formulario.contains(document.activeElement)) {
      return;
    }
    var campos = todos('input:not([type=hidden]), select, textarea', formulario);
    var campo = campos.filter(function (item) {
      return !item.disabled && !item.readOnly;
    })[0];
    if (campo && typeof campo.focus === 'function') {
      campo.focus();
    }
  }

  // -------------------------------------------------------- Modo manual (F276Id)
  // Muestra el campo "Nro. Acta" sólo cuando el formulario se abre en modo manual
  // (carga manual de turno). El modo se preserva en el campo oculto `manual`.
  function sincronizarModoManual() {
    var campoManual = document.querySelector('input[name="manual"]');
    var manual = campoManual !== null
        && (campoManual.value === 'true' || campoManual.checked === true);
    todos('.atp-acta-manual').forEach(function (elemento) {
      elemento.style.display = manual ? '' : 'none';
    });
  }

  // ------------------------------------------------------------- Activación

  function activar() {
    sincronizarModoManual();
    activarGrilla();
    activarConfirmacion();
    enfocarPrimerCampo();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', activar);
  } else {
    activar();
  }
})();
