package com.example.mygraduationproject.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mygraduationproject.model.ChatSession;

import java.util.List;

/**
 * 聊天会话数据访问对象，提供chat_sessions表的增删改查操作
 * 用于管理花烛、白掌、绿萝等植物相关的AI对话会话
 */
@Dao
public interface ChatSessionDao {

    /** 插入一条聊天会话，返回自增ID */
    @Insert
    long insert(ChatSession session);

    /** 更新一条聊天会话 */
    @Update
    void update(ChatSession session);

    /** 删除一条聊天会话 */
    @Delete
    void delete(ChatSession session);

    /** 按更新时间倒序获取全部聊天会话（最近更新的排在前面） */
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    List<ChatSession> getAllSessions();

    /** 根据会话ID获取聊天会话 */
    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    ChatSession getById(long id);

    /** 根据图片完整路径精确查找关联的聊天会话（限制1条） */
    @Query("SELECT * FROM chat_sessions WHERE imagePath = :imagePath LIMIT 1")
    ChatSession getByImagePath(String imagePath);

    /** 根据文件名模糊匹配查找聊天会话（用于签名URL与本地路径的兼容匹配） */
    @Query("SELECT * FROM chat_sessions WHERE imagePath LIKE '%' || :fileName || '%' LIMIT 1")
    ChatSession getByFileName(String fileName);

    /** 获取聊天会话总条数 */
    @Query("SELECT COUNT(*) FROM chat_sessions")
    int getCount();

    /**
     * 删除旧会话，仅保留最近更新的keepCount条
     * 通过子查询找出最近更新的keepCount条记录，删除不在其中的旧会话
     */
    @Query("DELETE FROM chat_sessions WHERE id NOT IN (SELECT id FROM chat_sessions ORDER BY updatedAt DESC LIMIT :keepCount)")
    void deleteOldSessions(int keepCount);

    /** 删除全部聊天会话 */
    @Query("DELETE FROM chat_sessions")
    void deleteAll();
}
