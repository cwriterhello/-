package com.hmdp.service;

public interface ILock {
    //尝试获取锁
    public boolean tryLock(Long timeOutSec);

    //释放锁
    public void unLock();
}
