package cn.iflyos.open.ota;

import android.text.TextUtils;

class PackageEntity {

    public long id;
    public long revision;
    public String identity;
    public String url;

    public boolean valid() {
        return !TextUtils.isEmpty(identity) && !TextUtils.isEmpty(url);
    }

}
