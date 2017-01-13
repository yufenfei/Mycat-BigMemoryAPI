package io.mycat.bigmem.cacheway.alloctor.recycle.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.mycat.bigmem.buffer.MycatBufferBase;
import io.mycat.bigmem.cacheway.alloctor.BufferPageBase;
import io.mycat.bigmem.cacheway.alloctor.BufferPageMoveInf;
import io.mycat.bigmem.cacheway.alloctor.recycle.MemoryPageClearUpInf;

/**
 * 使用标记-移动的算法进行内存块的回收
 * @author kk
 * @date 2017年1月3日
 * @version 0.0.1
 */
public class MarkMovePageRecycleImpl implements MemoryPageClearUpInf {

    @Override
    public void pageClearUp(BufferPageBase page) {
        // 检查当前是否已经实现了内存页的整理接口
        if (page instanceof BufferPageMoveInf) {
            // 找出前buffer中所有已经分配出去的内存对象信息
            BufferPageMoveInf movePage = (BufferPageMoveInf) page;

            Set<MycatBufferBase> bufferList = movePage.getSliceMemory();

            // 未使用的首个内存索引
            int notUseIndexStart = 0;

            // 已经使用内存块
            List<MycatBufferBase> useBuffer = new ArrayList<>(1);

            // 找到第一个未使用内存块信息,以及未使用的内存后，第一个已经使用的内存
            notUseIndexStart = this.getNotUseIndex(bufferList, notUseIndexStart, page, useBuffer);

            if (!useBuffer.isEmpty()) {
                MycatBufferBase useBufferBase = useBuffer.remove(0);
                // 进行首次的内存的移动
                ((BufferPageMoveInf) page).memoryCopy(useBufferBase, notUseIndexStart);
                notUseIndexStart = notUseIndexStart + useBufferBase.limit() / page.getChunkSize();

                // 从引用开始遍历
                Iterator<MycatBufferBase> bufferIter = bufferList.iterator();
                while (bufferIter.hasNext()) {
                    MycatBufferBase useItem = bufferIter.next();

                    if (useItem.address() > useBufferBase.address()) {
                        ((BufferPageMoveInf) page).memoryCopy(useItem, notUseIndexStart);
                        notUseIndexStart += useItem.limit() / page.getChunkSize();
                    }
                }

            } else {

            }

        }
    }

    /**
     * 获取已经使用索引编号信息
     * @param bufferList 所有引用对象
     * @param index 进行内存遍历的首个索引
     * @param notUseIndexStart 未使用索引号，从0开始遍历
     * @param page 内存页信息
     * @param useBuffer 找到的引用对象信息
     * @return 未使用的索引号
     */
    private int getNotUseIndex(Set<MycatBufferBase> bufferList, int notUseIndexStart, BufferPageBase page,
            List<MycatBufferBase> useBuffer) {

        // 获取最基本的内存地址
        long baseAddress = page.getBuffer().address();
        int chunkSize = page.getChunkSize();

        if (null != bufferList && !bufferList.isEmpty()) {

            // 从引用开始遍历
            Iterator<MycatBufferBase> bufferIter = bufferList.iterator();
            while (bufferIter.hasNext()) {
                MycatBufferBase useItem = bufferIter.next();

                // 如果按顺序的内存地址与集合中相同，说明当前块已经使用
                if ((notUseIndexStart * chunkSize + baseAddress) == useItem.address()) {
                    // 跳转至下一个内存块的开始处
                    notUseIndexStart += useItem.limit() / chunkSize;
                    continue;
                }
                // 找到空余内存块后的第一块内存块的信息
                else {
                    useBuffer.add(useItem);
                    break;
                }

            }
        }

        return notUseIndexStart;
    }

    private void printBuffer(Set<MycatBufferBase> bufferList) {
        for (MycatBufferBase mycatBufferBase : bufferList) {
            System.out.println(mycatBufferBase.address());
        }
    }

}
