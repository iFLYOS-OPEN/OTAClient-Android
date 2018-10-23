package cn.iflyos.open.ota;

import java.util.ArrayList;
import java.util.List;

class ReportEntity {

    public List<Long> pids = new ArrayList<>();

    static ReportEntity from(List<ManifestEntity> manifest) {
        final ReportEntity entity = new ReportEntity();
        for (ManifestEntity pkg : manifest) {
            entity.pids.add(pkg.id);
        }
        return entity;
    }

}
