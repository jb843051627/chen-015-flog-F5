package com.fc.v2.model.custom.flog;

import java.io.Serializable;

/**
 * 勾销卡起卡送报：只认底档字号 + 小班号这一对。
 * 卡一起就停在头一道口（报伐）待过；实采株数、实采蓄积不在此处塞，过报伐口时才报。
 *
 * @author fuce
 * @date 2026-10-01
 */
public class FlogCloseOpenDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 编制单位底档字号 */
    private String siteNo;

    /** 小班号 */
    private String subcompNo;

    /** 伐区名号（不填则过报伐口认设计单时照钉档补） */
    private String cutBlockNo;

    /** 备注 */
    private String remark;

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public String getSubcompNo() {
        return subcompNo;
    }

    public void setSubcompNo(String subcompNo) {
        this.subcompNo = subcompNo;
    }

    public String getCutBlockNo() {
        return cutBlockNo;
    }

    public void setCutBlockNo(String cutBlockNo) {
        this.cutBlockNo = cutBlockNo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
