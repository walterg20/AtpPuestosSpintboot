package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import java.util.List;

public record SmoothNavMenuData(
        String id,
        String title,
        String description,
        String link,
        String linkTarget,
        List<SmoothNavMenuData> items) {

    public SmoothNavMenuData {
        if (items == null) {
            items = List.of();
        }
    }
}
