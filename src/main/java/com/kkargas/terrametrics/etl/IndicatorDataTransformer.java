package com.kkargas.terrametrics.etl;

/** Compatibility entry point; dimensions and facts must be generated together for stable foreign keys. */
public final class IndicatorDataTransformer {
    private IndicatorDataTransformer() {}
    public static void main(String[] args) throws Exception { ClimateEtl.transform(); }
}
