package cn.iflyos.open.ota;

class ManifestEntity {

    public long id;
    public long revision;
    public String identity;

    static ManifestEntity from(PackageEntity pkg) {
        final ManifestEntity entity = new ManifestEntity();
        entity.id = pkg.id;
        entity.revision = pkg.revision;
        entity.identity = pkg.identity;
        return entity;
    }

}
