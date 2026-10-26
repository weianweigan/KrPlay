package com.krplay.client;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Vector stroke icons stay crisp at automotive display densities. */
final class DockIcon extends Drawable {
    private final String kind;private final Paint paint=new Paint(3);private int color;
    DockIcon(String k,int c){kind=k;color=c;paint.setStrokeWidth(1.8f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);}
    public void draw(Canvas canvas){canvas.save();Rect b=getBounds();canvas.translate(b.left,b.top);canvas.scale(b.width()/24f,b.height()/24f);paint.setColor(color);paint.setStyle(Paint.Style.STROKE);
        Path p=new Path();switch(kind){
            case "car":canvas.drawRoundRect(3,5,21,19,3,3,paint);line(canvas,7,9,17,9);line(canvas,7,13,11,13);canvas.drawCircle(17,14,1.5f,paint);break;
            case "apps":for(int x=4;x<=14;x+=10)for(int y=4;y<=14;y+=10)canvas.drawRoundRect(x,y,x+6,y+6,1.5f,1.5f,paint);break;
            case "back":p.moveTo(14,5);p.lineTo(7,12);p.lineTo(14,19);canvas.drawPath(p,paint);break;
            case "home":p.moveTo(3,11);p.lineTo(12,3);p.lineTo(21,11);p.moveTo(6,10);p.lineTo(6,21);p.lineTo(18,21);p.lineTo(18,10);p.moveTo(10,21);p.lineTo(10,14);p.lineTo(14,14);p.lineTo(14,21);canvas.drawPath(p,paint);break;
            case "voice":canvas.drawRoundRect(9,3,15,15,3,3,paint);canvas.drawArc(6,7,18,19,0,180,false,paint);line(canvas,12,19,12,22);break;
            case "performance":canvas.drawArc(3,3,21,21,180,180,false,paint);line(canvas,12,12,17,7);line(canvas,4,19,20,19);break;
            case "network":canvas.drawArc(2,4,22,24,225,90,false,paint);canvas.drawArc(6,8,18,24,225,90,false,paint);paint.setStyle(Paint.Style.FILL);canvas.drawCircle(12,19,1.4f,paint);break;
            case "settings":canvas.drawCircle(12,12,4,paint);for(int i=0;i<8;i++){double a=i*Math.PI/4;line(canvas,(float)(12+7*Math.cos(a)),(float)(12+7*Math.sin(a)),(float)(12+9*Math.cos(a)),(float)(12+9*Math.sin(a)));}break;
            case "refresh":canvas.drawArc(4,4,20,20,35,290,false,paint);p.moveTo(20,4);p.lineTo(20,10);p.lineTo(14,10);canvas.drawPath(p,paint);break;
            case "hide":p.moveTo(9,5);p.lineTo(16,12);p.lineTo(9,19);canvas.drawPath(p,paint);break;
            case "exit":canvas.drawArc(4,4,20,20,-55,290,false,paint);line(canvas,12,2,12,12);break;
            default:p.moveTo(15,5);p.lineTo(8,12);p.lineTo(15,19);canvas.drawPath(p,paint);
        }canvas.restore();
    }
    private void line(Canvas c,float a,float b,float x,float y){c.drawLine(a,b,x,y,paint);}
    public void setAlpha(int a){paint.setAlpha(a);invalidateSelf();}public void setColorFilter(ColorFilter f){paint.setColorFilter(f);invalidateSelf();}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
