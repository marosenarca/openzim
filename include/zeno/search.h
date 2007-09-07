/*
 * Copyright (C) 2007 Tommi Maekitalo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * is provided AS IS, WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, and
 * NON-INFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301 USA
 *
 */

#ifndef ZENO_SEARCH_H
#define ZENO_SEARCH_H

#include <zeno/article.h>
#include <vector>
#include <map>

namespace zeno
{
  class SearchResult
  {
      Article article;
      mutable double priority;
      struct WordAttr
      {
        unsigned count;
        unsigned addweight;
        WordAttr() : count(0), addweight(1) { }
      };

      typedef std::map<std::string, WordAttr> WordListType;
      typedef std::map<uint32_t, std::string> PosListType;
      WordListType wordList;
      PosListType posList;

    public:
      SearchResult() : priority(0) { }
      explicit SearchResult(const Article& article_, unsigned priority_ = 0)
        : article(article_),
          priority(priority_)
          { }
      const Article& getArticle() const  { return article; }
      double getPriority() const;
      void foundWord(const std::string& word, uint32_t pos, unsigned addweight);
      unsigned getCountWords() const  { return wordList.size(); }
      unsigned getCountPositions() const  { return posList.size(); }
  };

  class Search
  {
    public:
      class Results : public std::vector<SearchResult>
      {
          std::string expr;

        public:
          void setExpression(const std::string& e)
            { expr = e; }
          const std::string& getExpression() const
            { return expr; }
      };

    private:
      static double weightTitle;
      static double weightOcc;
      static double weightOccOff;
      static double weightPlus;
      static double weightDist;
      static double weightPos;
      static double weightDistinctWords;

      File indexfile;
      File articlefile;

    public:
      template <typename IndexfileInitT, typename ArticlefileInitT>
      Search(IndexfileInitT indexfile_, ArticlefileInitT articlefile_)
        : indexfile(indexfile_),
          articlefile(articlefile_)
          { }

      Results search(const std::string& expr);

      static double getWeightTitle()               { return weightTitle; }
      static double getWeightOcc()                 { return weightOcc; }
      static double getWeightOccOff()              { return weightOccOff; }
      static double getWeightPlus()                { return weightPlus; }
      static double getWeightDist()                { return weightDist; }
      static double getWeightPos()                 { return weightPos; }
      static double getWeightDistinctWords()       { return weightDistinctWords; }

      static void setWeightTitle(double v)         { weightTitle = v; }
      static void setWeightOcc(double v)           { weightOcc = v; }
      static void setWeightOccOff(double v)        { weightOccOff = v; }
      static void setWeightPlus(double v)          { weightPlus = v; }
      static void setWeightDist(double v)          { weightDist = v; }
      static void setWeightPos(double v)           { weightPos = v; }
      static void setWeightDistinctWords(double v) { weightDistinctWords = v; }
  };
}

#endif // ZENO_SEARCH_H