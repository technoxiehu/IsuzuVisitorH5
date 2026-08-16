package com.ruoyi.visitor.utils;

import java.util.regex.Pattern;

/**
 * 身份证号工具类（GB 11643-1999 校验位算法 + 展示脱敏）
 *
 * 脱敏规则与前端 isuzu-visitor-h5/src/utils/mask.js 保持一致（前 3 位 + 11 个 * + 后 4 位）。
 *
 * @author isuzu
 */
public final class IdCardUtils
{
    /** 18 位身份证格式：17 位数字 + 1 位数字或 X（末位小写 x 由调用方统一转大写后再校验） */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{17}[\\dX]$");

    /** 前 17 位加权因子（GB 11643-1999） */
    private static final int[] WEIGHTS = { 7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2 };

    /** 校验码字符表（索引 = 加权和 mod 11） */
    private static final String CHECK_CHARS = "10X98765432";

    private IdCardUtils()
    {
    }

    /**
     * 校验 18 位身份证号（格式 + 校验位算法），入参需已统一为大写
     *
     * @param idCard 身份证号
     * @return 是否合法
     */
    public static boolean isValid(String idCard)
    {
        if (idCard == null || !ID_CARD_PATTERN.matcher(idCard).matches())
        {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 17; i++)
        {
            sum += (idCard.charAt(i) - '0') * WEIGHTS[i];
        }
        return CHECK_CHARS.charAt(sum % 11) == idCard.charAt(17);
    }

    /**
     * 脱敏：保留前 3 位与后 4 位，中间 11 位替换为 *，如 110***********1234（末位 X 保留）。
     * 幂等：对已脱敏值调用结果不变（含 * 不匹配身份证格式，原样返回）。
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String mask(String idCard)
    {
        if (idCard == null || !ID_CARD_PATTERN.matcher(idCard).matches())
        {
            return idCard;
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(15);
    }
}
