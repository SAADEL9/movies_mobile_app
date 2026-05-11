-- Create watched table
CREATE TABLE IF NOT EXISTS public.watched (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id uuid REFERENCES auth.users NOT NULL,
    movie_id int NOT NULL,
    title text NOT NULL,
    poster_path text,
    rating float,
    watched_at timestamp with time zone DEFAULT now() NOT NULL
);

-- Enable RLS for watched table
ALTER TABLE public.watched ENABLE ROW LEVEL SECURITY;

-- Policies for watched table
CREATE POLICY "Users can only access their own watched movies" 
ON public.watched 
FOR ALL 
USING (auth.uid() = user_id);

-- Create ratings table
CREATE TABLE IF NOT EXISTS public.ratings (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id uuid REFERENCES auth.users NOT NULL,
    movie_id int NOT NULL,
    rating float NOT NULL,
    rated_at timestamp with time zone DEFAULT now() NOT NULL,
    UNIQUE(user_id, movie_id)
);

-- Enable RLS for ratings table
ALTER TABLE public.ratings ENABLE ROW LEVEL SECURITY;

-- Policies for ratings table
CREATE POLICY "Users can only access their own ratings" 
ON public.ratings 
FOR ALL 
USING (auth.uid() = user_id);
