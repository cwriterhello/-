package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService iSeckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;

    @Override
    @Transactional
    public Result seckillVoucher(Long voucherId) {
        //1查询秒杀优惠券
        SeckillVoucher voucher = iSeckillVoucherService.getById(voucherId);
        //2判断有效时间
        //3不在时间内，返回异常信息
        if (LocalDateTime.now().isBefore(voucher.getBeginTime())){
            return Result.fail("活动还未开始！");
        }
        if (LocalDateTime.now().isAfter(voucher.getEndTime())){
            return Result.fail("活动已经结束！");
        }
        //4查询库存
        if (voucher.getStock()<1){
            //5没有库存，返回异常信息
            return Result.fail("库存不足！");
        }
        //6库存-1，
        boolean success = iSeckillVoucherService.update()
                //set stock = stock -1 where stock >0 and voucher_id = ?
                .setSql("stock = stock - 1").gt("stock",0) //乐观锁，在更改数据库之前判断库存是否>0
                .eq("voucher_id",voucherId).update();
        if (!success){
            return Result.fail("库存不足");
        }
        //7创建订单，返回订单
        VoucherOrder order = new VoucherOrder();
        //7.1订单id
        long orderId = redisIdWorker.netId("order");
        order.setId(orderId);
        //7.2用户id
        long userId = UserHolder.getUser().getId();
        order.setUserId(userId);
        //7.3代金券id
        order.setVoucherId(voucherId);
        save(order);

        //返回订单id
        return Result.ok(orderId);
    }
}
