package org.example.entity;
import lombok.Data;
/**
 * 钉钉群 自定义机器人返回的结果
 * <p>
 * 正常的返回：{"errcode":0,"errmsg":"ok"}
 *
 * @author 3y
 */
@Data
public class DingDingRobotResult {
    /**
     * errcode
     */
    private Integer errcode;

    /**
     * errmsg
     */
    private String errmsg;
}
