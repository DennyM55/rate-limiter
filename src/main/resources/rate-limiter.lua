local key = KEYS[1]

local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local requestId = ARGV[4]

local cutoff = now - window

redis.call(
    'ZREMRANGEBYSCORE',
    key,
    '-inf',
    cutoff
)

local count = redis.call('ZCARD', key)

if count >= limit then
    local oldest = redis.call(
        'ZRANGE',
        key,
        0,
        0,
        'WITHSCORES'
    )

    local retryAfter =
        math.ceil((oldest[2] + window - now) / 1000)

    return {0, 0, retryAfter}
end

redis.call('ZADD', key, now, requestId)
redis.call('EXPIRE', key, math.ceil(window / 1000))

local remaining = limit - count - 1

return {1, remaining, 0}