package com.muen.greedysnake.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.muen.greedysnake.entity.Direction
import com.muen.greedysnake.entity.GameStyle
import com.muen.greedysnake.entity.Type
import com.muen.greedysnake.rxbus.RxBus
import com.muen.greedysnake.rxbus.event.GameOver
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs
import kotlin.random.Random

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val gameSize = 14 // 地图的长宽
    private var screenHeight = 0 // 屏幕的整体高度
    private var screenWidth = 0 // 屏幕的整体宽度

    private val map = arrayListOf<ArrayList<GameStyle>>() // 整个地图的元素
    private var snakeLocation = arrayListOf<Point>() // 蛇的位置
    private val snakeHead = Point(gameSize / 2, gameSize / 2) // 蛇头位置
    private var foodLocation = Point() // 食物位置

    private var moveSpeed = 4 // 移动速度
    private var snakeLength = 4 // 蛇的长度
    private var snakeDirection = Direction.UP // 移动方向

    private var eatCount = 0 // 吃的食物数量

    private var gameStart = false // 游戏是否开始

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG) // 画笔

    private var timer: Timer? = null

   init {
       initGame()
   }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val blockWidth = screenWidth / gameSize // 每个网格的宽度
        val blockHeight = screenHeight / gameSize // 每个网格的高度

        // 绘制地图元素
        for (y in 0 until gameSize) {
            for (x in 0 until gameSize) {
                // 每个矩形的范围
                val left = x * blockWidth.toFloat()
                val right = (x + 1f) * blockWidth
                val top = y * blockHeight.toFloat()
                val bottom = (y + 1f) * blockHeight

                // 不同的标识设置不同的画笔样式
                when (map[y][x].type) {
                    Type.GRID -> mPaint.style = Paint.Style.STROKE
                    Type.FOOD, Type.BODY -> mPaint.style = Paint.Style.FILL
                }
                // 根据标识设置画笔颜色
                mPaint.color = map[y][x].getColor()

                // 当前的位置是否为头部
                if (x == snakeHead.x && y == snakeHead.y) {
                    mPaint.style = Paint.Style.FILL
                    mPaint.color = GameStyle(Type.HEAD).getColor()
                }

                // 绘制矩形
                canvas.drawRect(left, top, right, bottom, mPaint)
            }
        }
        invalidate()
    }

    /**
     * 在onSizeChanged可以获取到外部给GameView设置的宽高，所以这里给先前创建的变量进行赋值
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenHeight = height
        screenWidth = width
    }

    /**
     * 初始化函数
     */
    private fun initGame() {
        // 地图初始化
        for (y in 0 until gameSize) {
            val styleList = arrayListOf<GameStyle>()
            for (x in 0 until gameSize) {
                styleList.add(GameStyle(Type.GRID)) // 默认全部为格子
            }
            map.add(styleList)
        }
        // 随机食物的位置
        randomCreateFood()

        // 蛇头位置更新到蛇身上
        snakeLocation.add(Point(snakeHead.x, snakeHead.y))

        gameStart = true
        setListener()   //启动控制器
        startGame()
    }

    private fun startGame() {
        if (timer != null){
            timer?.cancel()
            timer = null
        }
        timer = Timer()
        timer?.schedule(object: TimerTask() {
            override fun run() {
                if(gameStart){
                    moveSnake() // 移动蛇
                    drawSnakeBody() // 绘制蛇身
                    refreshBody() // 刷新蛇身
                    judgeEat() // 判断吃
                    postInvalidate() // 刷新视图
                }
            }

        }, 0,1000 / moveSpeed.toLong())
    }

    /**
     * 重新开始游戏
     */
    fun restartGame(){
        //重置游戏数据
        map.clear()
        snakeHead.x = gameSize / 2
        snakeHead.y = gameSize / 2
        snakeLocation = arrayListOf<Point>()
        moveSpeed = 4 // 移动速度
        snakeLength = 4 // 蛇的长度
        snakeDirection = Direction.UP // 移动方向
        //初始化游戏
        initGame()
    }

    /**
     * 随机生成食物
     */
    private fun randomCreateFood() {
        var food = Point(Random.nextInt(gameSize), Random.nextInt(gameSize))
        var index = 0
        while (index < snakeLocation.size - 1) {
            if (food.x == snakeLocation[index].x && food.y == snakeLocation[index].y) {
                food = Point(Random.nextInt(gameSize), Random.nextInt(gameSize))
                index = 0
            }
            index++
        }

        foodLocation = food
        refreshFood()
    }

    /**
     * 食物更新到地图上
     */
    private fun refreshFood() {
        map[foodLocation.y][foodLocation.x].type = Type.FOOD
    }

    /**
     * 移动
     */
    private fun moveSnake() {
        when (snakeDirection) {
            Direction.LEFT -> {
                if (snakeHead.x - 1 < 0) {
                    snakeHead.x = gameSize - 1
                } else {
                    snakeHead.x = snakeHead.x - 1
                }
                snakeLocation.add(Point(snakeHead.x, snakeHead.y))
            }
            Direction.RIGHT -> {
                if (snakeHead.x + 1 >= gameSize) {
                    snakeHead.x = 0
                } else {
                    snakeHead.x = snakeHead.x + 1
                }
                snakeLocation.add(Point(snakeHead.x, snakeHead.y))
            }
            Direction.UP -> {
                if (snakeHead.y - 1 < 0) {
                    snakeHead.y = gameSize - 1
                } else {
                    snakeHead.y = snakeHead.y - 1
                }
                snakeLocation.add(Point(snakeHead.x, snakeHead.y))
            }
            Direction.DOWN -> {
                if (snakeHead.y + 1 >= gameSize) {
                    snakeHead.y = 0
                } else {
                    snakeHead.y = snakeHead.y + 1
                }
                snakeLocation.add(Point(snakeHead.x, snakeHead.y))
            }
        }
    }

    /**
     * 绘制蛇的身体
     */
    private fun drawSnakeBody() {
        var length = snakeLength
        for (i in snakeLocation.indices.reversed()) {
            if (length > 0) {
                length--
            } else {
                val body = snakeLocation[i]
                map[body.y][body.x].type = Type.GRID
            }
        }

        length = snakeLength
        for (i in snakeLocation.indices.reversed()) {
            if (length > 0) {
                length--
            } else {
                snakeLocation.removeAt(i)
            }
        }
    }

    /**
     * 身体更新到地图上
     */
    private fun refreshBody() {
        // 减1是因为不需要包括蛇头
        for (i in 0 until snakeLocation.size - 1) {
            map[snakeLocation[i].y][snakeLocation[i].x].type = Type.BODY
        }
    }

    /**
     * 吃判断，吃的是食物还是自己
     */
    private fun judgeEat() {
        // 是否吃到自己
        val head = snakeLocation[snakeLocation.size - 1]
        for (i in 0 until snakeLocation.size - 2) {
            val body = snakeLocation[i]
            if (body.x == head.x && body.y == head.y) {
                gameStart = false // 吃到身体游戏结束
                RxBus.get().post(GameOver())
            }
        }

        // 吃到食物
        if (head.x == foodLocation.x && head.y == foodLocation.y) {
            snakeLength++ // 长度+1
            randomCreateFood() // 刷新食物
        }
    }

    /**
     * 外部设置移动方向的方法
     */
    fun setMove(direction: Int) {
        when {
            snakeDirection == Direction.LEFT && direction == Direction.RIGHT -> return
            snakeDirection == Direction.RIGHT && direction == Direction.LEFT -> return
            snakeDirection == Direction.UP && direction == Direction.DOWN -> return
            snakeDirection == Direction.DOWN && direction == Direction.UP -> return
        }
        snakeDirection = direction
    }

    /**
     * 监听Touch事件
     */
    private fun setListener() {
        setOnTouchListener(object : OnTouchListener {
            private var staX = 0f
            private var staY = 0f
            private var endX = 0f
            private var endY = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        //记录起始位置
                        staX = event.x
                        staY = event.y
                    }

                    MotionEvent.ACTION_UP -> {
                        //记录终点位置
                        endX = event.x
                        endY = event.y
                        var swiped = false //记录是否有效滑动了

                        //水平移动更多
                        if (abs(endX - staX) > abs(endY - staY)) {
                            if (endX - staX > 10) {
                                //右移
                                setMove(Direction.RIGHT)
                            } else if (endX - staX < -10) {
                                //左移
                                setMove(Direction.LEFT)
                            }
                        } else {
                            if (endY - staY < -10) {
                                //上移
                                setMove(Direction.UP)
                            } else if (endY - staY > 10) {
                                //下移
                                setMove(Direction.DOWN)
                            }
                        }
                    }
                }
                return true
            }
        })
    }

}