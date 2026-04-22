package com.cpt202.app.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;

@Service
public class PriceService {

    /**
     * 根据时薪和起止时间计算总价
     * @param hourlyFee 每小时费用
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 总金额
     */
    public BigDecimal calculateTotalAmount(BigDecimal hourlyFee, LocalTime startTime, LocalTime endTime) {
        // 1. 计算分钟差
        long minutes = Duration.between(startTime, endTime).toMinutes();

        // 2. 转换为小时（保留两位小数，四舍五入）
        // 例如：90分钟 = 1.5小时
        BigDecimal hours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        // 3. 计算金额：时薪 * 小时数
        return hourlyFee.multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }
}